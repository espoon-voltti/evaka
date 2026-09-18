<!--
SPDX-FileCopyrightText: 2017-2026 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# Declarative data retention

A design for deleting personal data from eVaka once its retention period has passed: expiration rules declared per table and evaluated over a graph of one person's data, instead of deletion code written separately for each case.

## 1. Model, definitions and conceptual algorithm

### 1.1 Schema definition

A *schema definition* declares a set of database tables and the references between them as code, matching the database schema. A reference is directional: it goes from the table with the foreign key column to the table that is referenced. Each table definition also holds manually defined configuration such as *expiration rules* for when its data can be deleted.

The schema definition is also meant to be read as documentation. Most things are declared explicitly and then validated, rather than deduced silently, so that every detail that matters is visible in one place.

The `person` table is the *root* of this graph. All the other tables have references towards the root, directly or transitively. The root itself references nothing. 

A table is included in the schema definition if it can reach the `person` table by following the foreign keys. Tables like `daycare` are not included since they have no references towards person.

References to tables outside the declared graph are not declared. If a table points to a table outside the graph, and the referenced row would be left as an orphan without anything else referencing it, then the two can be deleted together by setting a separate `orphansToDelete` property.

The `child` table is a special case, which shares its primary key with `person`. References to people should either point to `child` or directly to `person`, depending on whether that person reference is in the role of child (e.g. `application.child_id`) or adult (e.g. `application.guardian_id`). Every person that has data related to them as a child should have a row in the `child` table. In the current database schema, neither of these assumptions currently fully hold, so manual exceptions must be defined for references that should actually be pointing to `child`, and a missing `child` row must be handled gracefully.

References must not form a cycle: following them from any table always ends at the root. A self-reference is never declared; the few that exist are handled case by case. The shape is therefore a directed acyclic graph (DAG). In practice, the tables currently form something close to a tree, where only a few tables reference multiple tables.

The tables that a table references, directly or transitively, are its *ancestors* (i.e. closer to the root), and the tables with references leading to it are its *descendants*. Because of the foreign key constraints, deletion always proceeds leaves first: a row cannot be deleted while some descendant table's row references it. 

A table can also name an ancestor in a property `bundledBy`. Its rows are then always deleted together with the rows of that ancestor, never one without the other.

By default, all the person's rows in a table are evaluated and deleted as a whole, for example, a certain period after the child's last placement ends. If the expiration rule is such that different rows should be deleted independently of each other at different times, for example, a certain period after the creation, then a property `independentRows` can be set.

### 1.2 Person graph

One run of the algorithm handles one *target person*. The person is identified by their person id, which is the primary key of both `person` and `child`. The run builds a *person graph*. It has the shape of the schema definition and is filled with the rows that relate to the target person.

A *node* holds rows of one table. There are two kinds of nodes. An *own node* holds rows that are handled by this person and can be deleted in this run. If the table uses independent rows, then each of them gets a separate node. A *foreign node* holds rows that are owned by another person and won't be deleted in this run, but which still reference and block the deletion of some other rows in the graph. If the table has multiple such blocking references, then each gets their own foreign node.

A graph edge goes from a node to every node that holds a row that one of its rows references through a blocking reference. A node that holds no rows will not be part of the graph.

The rules of each table are then evaluated over the graph. An own node whose expiration rule is met is *expired*. For a node to be deletable, every other node that has a blocking reference to it or bundles with it must also be expired and deletable. Nodes that are expired but cannot be deleted are considered *blocked* by another node. At the end, all the deletable nodes are deleted, leaves first. Chapter 4 describes the evaluation in detail.

### 1.3 Handlers and reference kinds

Many rows reference multiple people, either directly or transitively. Writing rules, such as when an application should be deleted, gets difficult unless we know from which perspective it is being evaluated: is the target person the child, the guardian, or the guardian's partner? Things become easier if only one role always handles the deletion of data in some table. 

Therefore, each table whose rows this algorithm deletes gets assigned a *handler* of either child or adult in a property `handledBy`. These are the *handled tables*. The table `person` is always handled by adult and `child` by child.

Each reference is declared as one of three kinds with `primaryReference`, `secondaryReference` or `optionalReference`. Each handled table other than `person` declares exactly one *primary reference*. Both tables connected by a primary reference must have the same handler (except from child to person), which will be also the reference's role. For example, an adult-handled `fee_decision` references `person` through both `head_of_family_id` and `partner_id`, so it must declare one as primary reference and the other as *secondary reference*. Both primary and secondary references block the deletion of the rows they reference, so they are called *blocking references*.

The third possible reference kind is an *optional reference*, which is a nullable foreign key column. It doesn't block the deletion of the rows it references, as the column can be set to null instead. 

Some tables with references towards person are handled outside this algorithm. Those are declared as *external* tables. This algorithm never deletes their rows, but some other job is expected to do it. An external table has no handler and no primary reference, and other tables can't reference it. Its secondary references still block the rows they reference, so the table must still be declared. Its optional references are cleared like any other.

In a person's run, a table's *own rows* are the rows that lead to the target person through primary references, and they form the table's own node. They are found by starting from the person row and following the primary references backwards towards the leaves. The `person` node always holds exactly one row, and the `child` node one or none. A table with no own rows has no own node (except `child`).

Rows of the same table that reach the target person through a secondary reference are *foreign*. The foreign rows found through one secondary reference form a foreign node, which blocks the own nodes it references as long as it has rows. Foreign rows will be deleted only by another person's run, or in another job if the table is external.

Following primary references always leads to the target person through tables which all have the same handler. This *primary path* is unique for every table, so the primary references form a tree. If the handler is child, then the path goes to `person` through `child`. If the handler is adult, then it goes directly to `person` without passing through `child`. Apart from the shared `person` root, the graph therefore splits into two separate parts, where one holds data about them as a child, and the other as an adult. This split lets us write the data expiration rules for each table from a known perspective of child or adult.

Some young adults might have data about them in both roles, so in one run a table may have both own and foreign rows. The own rows form the table's own node as usual, and the foreign rows form foreign nodes. For example, `application` can contain both applications where they were the child and more recent applications that they made for their own children. The first ones are own rows while the others are foreign rows through the guardian reference.

When a table leads to both child and adult, then picking the handler is a judgment call. As a rule of thumb, it should be the one whose data it primarily is, and against whose graph its rules can be written more easily. Where there is no clear answer, child is a good default.

### 1.4 Deletion bundles

The ancestor that `bundledBy` names must lie on the table's primary path. The declaration is an additional virtual reference from the ancestor back to the table, in the opposite direction to the real one, which forces a cyclic dependency. As a result, the descendant also cannot be deleted before the ancestor is deleted, so both need to be deleted at the same time. If the table has no other rules of its own, but should simply be deleted once the ancestor can be deleted, then the expiration rule can be set to `Always`.

If there are nodes connecting the two nodes, none of them can be deleted independently either. All of these tables form an atomic *deletion bundle*: in a person's run, either the rows of the entire set are deleted, or none are. If a table outside the bundle bundles one of its tables, or is bundled by one of them, then it and all the tables between them become part of the same bundle.

Because the bundling table lies on the primary path of the bundled table, the two always have the same handler, and whenever a bundled row is loaded in a run, the row that bundles it is loaded too. 

As an example, `fee_decision_child` is expected to declare `bundledBy = fee_decision`, so that a decision and its child rows are always deleted together. `fee_decision_child` is then adult-handled through its reference to `fee_decision`, and its reference to `child` is secondary. In the child's run, `fee_decision_child` rows are thus foreign, while `fee_decision` has no node at all, since none of its rows have references leading to `child`.

### 1.5 Independent rows

When a table is evaluated as a whole, all of its own rows form one node and are deleted at the same time. This also limits what kind of expiration rules are available. The result cannot depend on a state specific to one row, but must be an aggregate that is the same for all the rows in the node. The rule can state, for example, that all of child's placements must have ended at least ten years ago or that child must be at least ten years old. But it cannot state, for example, that the row would be deleted two years after its own end date, independent of the other rows in the same table.

This is allowed for a table with `independentRows`. Each of its rows then gets a node and a verdict of its own: the rules may depend on the row itself, and the rows expire and are deleted independently of each other. Edges follow the general rule, so a row has edges only to the nodes holding the rows it references, and a foreign node blocks only the rows its rows reference.

With a graph A<-B<-C, where B has independent rows while A and C are evaluated as a whole, no rows in A can be deleted before every row of B has been deleted, and no row of B can be deleted before all of C is deleted. However, once C has been fully deleted and no longer blocks, then the rows of B can be deleted individually.

Next, let's assume that B references A and A bundles B. Bundling follows the primary references row by row. If both have independent rows, then a row of A bundles the rows of B that reference it specifically, creating multiple small deletion bundles. If either of the tables is evaluated as a whole, or there is any such table between them, then it follows that every node must still be deleted together and they are all part of the same deletion bundle. This makes the row level evaluations fully redundant and wasteful, so validation of the schema definition checks that within a deletion bundle either every table has `independentRows` or none of them has.

### 1.6 Optional references

An optional reference is declared with `optionalReference`. It does not keep the rows it references alive: when they are deleted, the algorithm first sets the column to null in every row that references them, so the column must be nullable. The reference has no role, it is neither primary nor secondary, and it plays no part in finding rows or in ordering them. If the column already has `ON DELETE SET NULL`, declaring it as an optional reference is not mandatory, but still usually recommended, because an explicit update leads to more accurate audit logs.

Placement, for example, stores which application created it in `placement.source_application_id`. This is meta-level data, which should not block the application from being deleted earlier. A check constraint forbids `source = 'APPLICATION'` without the id, so the reference also lists `source` in `alsoNull` property. All of those columns are then set to null in one statement and in the same transaction where the application is deleted.

## 2. Expiration rules

Every handled table defines an *expiration rule*, which decides in the run of the target person whether the table's own nodes have expired. An external table has no rule, since its rows are never evaluated here.

The following are examples of different kinds of expiration rules. They can be combined with rules such as all of, any of and coalesce.

### 2.1 Always and never

The simplest rules return a constant. Under `Always` the nodes are expired from the start and are deleted as soon as nothing blocks them, which suits a table such as `fee_decision_child` whose rows are deleted together with the row that bundles them. A table that is not bundled must not use `Always`. Under `Never` a node never expires, which is mostly useful as a building block when rules are combined.

### 2.2 Reference date from a date source

Under a reference date rule a node expires once a given period has passed since a reference date. The rule defines the length of the period and the *date source* that gives the date. The period may be zero when the source gives the end of retention itself.

A date source is one of three kinds:

- A column of the table itself, such as the row's own end date or creation time.
- A column of another table of the graph, read as the latest date over all of that table's rows in the graph. The other table must be either `person` or a table with the same handler, since only their rows are in the graph of the run. The end date of the child's last placement is the most common one. Child's date of birth would be another example.
- A query written for the table. It is a function that takes the primary keys of the rows as input and returns a date for each of them. This can be used, for example, when the retention period of a child document is defined in its template, or when a complex rule depends on multiple people from the same family.

A node with independent rows gets the date of its own row, and a node evaluated as a whole the latest date over its rows. A source that reads another table gives every node the same date. A source has no date for a node when any of the rows it reads lacks a date, or when the other table has no rows in the graph.

A column source also says how the column is read. A date column gives the date as it is. A timestamp column is converted to a date in Finnish time. A date range column gives its inclusive end date, so an open-ended range gives no date. A range column whose end is never open is declared as a finite range instead.

A source may have no date for a node. A child may have no placements at all, if they e.g. decided not to start after all and the placement was deleted or the child is just a sibling of a placed child. The rule then cannot be evaluated and returns null instead of a verdict. Such a rule must be wrapped in coalesce which gives it a fallback. The fallback could, for example, count from the latest creation time among the node's rows, or be `Always` if the data should not exist without a placement.

Null never reaches all of or any of: validation checks that such a rule is wrapped directly in a coalesce rule, whose fallback is guaranteed to return a verdict. Whether a source may have no date is judged from the database schema, except for a query source, which declares it. 

### 2.3 Archived if required

Child documents, placement decisions, fee decisions and voucher value decisions can be archived to an external system. Under this rule a node is not expired while any of its rows is still to be archived.

### 2.4 Safe for integrations

Koski and Varda read a fixed set of tables. The rule of every table an integration reads is wrapped in a rule that names the integration. The node then expires only if the wrapped rule is met and every child the node concerns is *safe* for each named integration. More details in section 3.3.

## 3. Deletion instructions

An own node is deleted by the primary key of its rows, which the loader (section 4.1) reads together with the date columns the rules need.

The primary key is `id` unless the table declares otherwise. This can be a overridden with a different column or multiple columns in the case of a composite primary key. If the table has no primary key, any set of columns can be used that uniquely identify the row. However, a reference can only lead to a table whose primary key is just one column, so tables with multiple identifying columns must be leaves.

A table may also define following kinds of additional side effects to do together with the deletion.

### 3.1 Tables with external files

When rows that point to external files are deleted, such as `child_images` rows or decisions with a PDF, an async job that deletes the files must be queued in the same transaction. The table names the text columns to read from every deleted row and the jobs to plan from their values.

### 3.2 Orphans outside the schema definition

A row of a table outside the schema definition that is only referenced by one row of the declared tables and nothing else is left behind as an orphan when the referencing row is deleted. One such example would be `child_document_decision` of a child document. Whether that is acceptable or the row must be deleted too is decided case by case. 

When the orphan must be deleted, the referencing table names the reference among its *orphans to delete*, and the row is deleted right after the row that referenced it.

### 3.3 External integrations

Koski and Varda read a fixed set of tables. Deleting rows from any of them must permanently freeze that integration for the child, so that the deletion is not sent to the external system. The safe for integrations rule (section 2.4) guarantees that this only ever happens for a child past the safe data removal age. The freeze is recorded only for a child who has been sent to the integration: a child never sent has nothing there to protect, and the freeze would keep the child out of the sync for good. Each freeze is a timestamp column on the `child` row, written in the same transaction as the deletions. A plan that deletes the `child` row itself records no freeze for that child, since the row itself is deleted.

The rows are not always deleted in the child's own run. When an adult's run deletes a fee decision, for example, the freeze must be recorded for every child on that decision.

The tables that hold the state of each sync, `koski_study_right`, `koski_upload_error` and `varda_state`, are bundled by `child`, so that all of them are deleted together, and never before the child has reached the safe data removal age. The `child` row may still be recreated without the freeze timestamps, for example if a guardian logs in before the child turns 18. Creating new placements for such a child is not realistic, but as an additional safeguard both integrations refuse to start syncing a child past that age who has no previous integration state rows. First sync happening at that age is clearly an error.

## 4. Running the algorithm

Each person is processed by an async job of its own, carrying only the person id and whether the run is a dry run. During early testing the jobs are queued by hand; later a nightly scheduled job queues them. How to pick candidate persons and cycle through them is left for later.

### 4.1 Processing one person

One run goes through four steps. All of them run in one transaction. Evaluation is expected to be fast, so there is no need to load the data in a separate read-only transaction.

**Step 1: load the person's graph.** The *loader* starts from the person row. The `child` node is identified by the person id whether or not the child row exists. The tables are then loaded one at a time in root-first order along the primary references. A table's own rows are the rows whose primary reference column holds the id of a row already loaded from the referenced table. For every own node the loader reads:

- the ids of its rows,
- the dates its rules read,
- the ids its rows reference,
- the ids of its orphans to delete,
- the columns its async jobs need.

It then runs the query sources and archived if required rules of the table for the rows. The edges of the graph are derived from the referenced ids.

Foreign rows are found next. The loader goes through every secondary reference whose target table has an own node. The rows that hold the id of one of those rows in the reference column, apart from the own rows, form a foreign node.

Finally, for the target and every child the own rows reference, the loader reads the date of birth and whether the child has been sent to Koski and to Varda, which the safe for integrations rules and the freezes need. Nothing needs to be checked for bundles: the validation of chapter 7 guarantees that a bundling row is loaded whenever a bundled row is.

**Step 2: drop the empty nodes.** Nodes with no rows are dropped from the graph: they have nothing to delete and reference nothing, so they take no part in the evaluation. The `child` node is however kept even when the child row is missing, so that blocking and bundling pass through it to `person` (section 6.1). With no rows it counts as expired, so it blocks nothing on its own.

**Step 3: evaluate the rules.** The rules are evaluated over the loaded graph in the two passes described below. This is a pure function of the loaded data, the rules and today's date. Its result is a *plan*:

- the nodes to delete in leaf-first order,
- the optional references to clear,
- the orphans to delete,
- the freezes to record,
- the async jobs to queue.

**Step 4: execute the plan.** In dry-run mode the plan is logged instead of executed.

### 4.2 First pass: expiration

An own node is *expired* when its expiration rule (chapter 2) is met. Only the node's own rule matters here, together with the data the rule reads, such as the child's placements. A foreign node never expires.

Nodes with no rows were dropped before this pass, so a foreign node exists only while its rows do.

### 4.3 Second pass: blocking

An expired node still cannot be deleted if a node that references it is not expired, because its rows are still referenced. We say the non-expired node *blocks* it: the expiration mark is removed, and the node in turn blocks the nodes it references. Likewise, a node that is not expired blocks the nodes it bundles.

```
for each loaded node n:
    n is expired if it is not a foreign node and its expiration rule is met

for each node n that is NOT expired after the first pass: block(n)

block(n):
    for each node m that n references and each node m that n bundles:
        if m is expired:
            m is no longer expired
            block(m)
```

Every node goes through `block` at most once, either from the outer loop or when it loses its mark, so the work is linear in the size of the graph. This is the mark phase of a [mark-and-sweep garbage collector](https://en.wikipedia.org/wiki/Tracing_garbage_collection#Naive_mark-and-sweep), with the non-expired nodes as its roots: what they reach stays, and the rest is swept.

The nodes that are still expired at the end are deleted leaf-first, in an order that follows the real edges only. They form the largest set in which every node's own rule is met, every node referencing a node in the set is in the set too (no foreign key violations on deletion), and every deletion bundle is either entirely in the set or entirely outside it.

### 4.4 Executing the plan

The nodes are deleted in leaf-first order, each by the primary keys of its rows (chapter 3). Before a node's rows are deleted, the optional references into it are cleared (section 1.6): the column and its `alsoNull` columns are set null in every row that references the node, whether or not the row was loaded. After them, the orphans its rows referenced are deleted by their primary keys (section 3.2). The integration freezes in the plan are written for the children the deleted rows concern, except one whose `child` row the plan deleted (section 3.3).

### 4.5 Failures

The async job runner can be set to a small number of retries, which helps with transient errors and deadlocks.

If the schema definition is incomplete, an unknown foreign key could cause a constraint violation when deleting rows it references. The transaction then safely rolls back. However, many columns have `ON DELETE CASCADE` or `SET NULL`, and in those cases there is no error: rows could be silently deleted or orphaned. This is why the schema definition is validated against the database schema (chapter 7).

A row inserted while the run is in progress is not deleted unseen, since rows are deleted by the ids the loader read (chapter 3). The new row stays. If the run deletes the row it references, the foreign key constraint fails and the transaction rolls back, so that the retry sees the new row. Where the database key cascades instead, the new row would be deleted with the rows it references. That risk is accepted: the runs happen at night, and a person whose data has expired has been inactive for years.

### 4.6 Concurrency

Runs are serialised: the jobs run in a pool of one, and the transaction takes an advisory lock before loading anything, which holds across every service instance. Clearing optional references may touch rows of other persons, and foreign nodes are read from them, so two runs for members of the same family could otherwise interfere.

## 5. Outside the graph or handled elsewhere

Some tables that reach `person` are still left out of the schema definition, and some rows in it are never deleted by any run. This chapter lists them.

### 5.1 Rows whose primary reference is null

A primary reference may be nullable, and a row where it is null is nobody's. No run finds it as an own row, so this algorithm never deletes it. Such rows are not personal data: a `calendar_event_attendee` may relate to a unit or a group instead of a child, and a `calendar_event_time` without a child is an unreserved discussion slot. Where such a row references own rows through a secondary reference, it is a foreign row and blocks them like any other.

### 5.2 Tables that are only referenced

A table that the graph only references, and that references nothing in it, is outside the graph. Whether its rows are left as orphans or deleted with the rows that reference them (section 3.2) is decided case by case:

- Case processes and their history rows are left as orphans. At least the ones from the current year must stay, so that the sequence numbers are not reused.
- A `child_document_decision` is deleted with its child document.
- A `voucher_value_report_snapshot` is referenced by many decisions through its `voucher_value_report_decision` rows, an external table, and needs a job of its own (section 8.6).
- A `calendar_event` is left as an orphan.

### 5.3 Tables excluded by exception

`attachment` and `sfi_message` reference several tables of the graph through separate columns, of which only one is set per row, and `sfi_message_event` follows `sfi_message`. Supporting them would need a way to split a table's rows by the column that is set, for only two cases, so they are excluded by exception. Their foreign keys are refused in the validation of chapter 7, and the database takes care of their rows instead. Every foreign key from `attachment` into the graph is `ON DELETE SET NULL`, and a separate job deletes attachments that no longer belong to anything. The foreign keys of `sfi_message` and `sfi_message_event` are `ON DELETE CASCADE`, so that a message is deleted with what it was sent for or with the guardian it was sent to.

A leaf table whose rows should simply be deleted with the rows they reference can be excluded the same way with `ON DELETE CASCADE`, as long as the rows need no rule and no deletion instruction of their own. `invoiced_fee_decision` and a citizen's login rows such as `citizen_user` and `citizen_passkey_registration` are excluded like this.

### 5.4 The evaka_user row

`evaka_user` reaches `person` through `citizen_id`, but it is excluded too. The database sets the column null when the person row is deleted, and the row lives on as the author of what the citizen created. Whether its name should then be anonymised is open (section 8.4). References to `evaka_user`, such as `created_by` columns, are therefore not declared.

### 5.5 Data in the child table

The `child` row is deleted at the very end, but some information in it, such as diet, may need to be deleted earlier. That information can be cleared by a separate job.

## 6. Special cases

### 6.1 Missing child row

Not every person with data as a child has a `child` row. The row is created when a guardian logs in, when a placement is created and in a few other places, but not for a sibling who only gets a parentship from an application, nor when a parentship or family contact is added by hand. Such a person might still have rows like `fridge_child` and `family_contact` that belong to them as a child, but which currently reference `person` directly.

Therefore the `child` node is part of every graph, identified by the person id, with one row or none. References to `child` find their rows by that id whether or not the row exists, and the node blocks and bundles like any other. With no rows it counts as expired, and deleting it does nothing. Nothing is sent to Koski or Varda about such a child, so the missing freeze columns do not matter.

### 6.2 Partnerships

A partnership is stored as two `fridge_partner` rows, one for each partner, which reference each other through a composite self-reference. Each row references its own person, so it is an own row of that partner's run. The table has `independentRows`, and the declared primary key of a row is the partnership id rather than its own id, so deleting it deletes both rows of the partnership at once, in whichever partner's run first finds the rule met. The other partner's run then finds no row. The rule reads only what the two rows share, the partnership's own dates and the placements of the children of either partner, so both runs reach the same verdict. The self-reference is not declared, and the schema test lists it as the only composite foreign key.

## 7. Validation

The schema definition is validated when it is constructed:

- `person` and `child` have their fixed shape, every table declares a primary key, no column carries two references, no reference is a self-reference, and every reference leads to a declared table whose primary key is one column.
- References form no cycle, and every table reaches the root through them.
- Every handled table other than `person` declares exactly one primary reference, and its role is the table's handler. `child` is the exception, since its primary reference leads to `person`. An external table has no handler and declares no primary reference.
- `bundledBy` names an ancestor on the table's primary path.
- Within a deletion bundle, either every table has `independentRows` or none has.
- A date source that reads another table reads `person` or a table with the same handler, and a column is read the same way everywhere. A query source and an archived if required rule need a primary key of one column.
- An orphan to delete points outside the schema definition from a column that is not a declared reference.

Unit tests check the declared schema definition:

- Every table an integration reads has the safe for integrations rule directly, and no table names an integration that does not read it.
- Every table whose rule is `Always` is bundled.

Integration tests then compare the schema definition against the database schema:

- Every foreign key into a table whose rows are deleted here is declared as a reference, handled by the database with `ON DELETE CASCADE` or `SET NULL` (section 5.3), refused, or listed as not yet declared. A key not yet declared is `NO ACTION` or `RESTRICT`, so that a run reaching its rows fails and rolls back. The ones that cascade are listed separately, and the job must not run on real data before they are declared. The only composite foreign key is the partnership self-reference (section 6.2).
- Every declared reference is a real foreign key to the primary key column of the declared table, or to either `child` or `person` when declared to `child`, and its column is indexed.
- The declared primary key of a table is unique in the database, as its real primary key or a unique constraint or index, alone or together with the primary reference to the target person, and its columns are uuids.
- An orphan to delete is the only foreign key into its table and points at the orphan's declared primary key column, which is a uuid, from a unique column.
- The column of an optional reference and its `alsoNull` columns are nullable.
- Every column a date source reads exists with the declared type. A column read as a finite date range is not null and has a check constraint that forbids an open end.
- A rule whose source may have no date is wrapped directly in a coalesce rule whose fallback always returns a verdict. A column source may have no date when the column is nullable or a date range, and one that reads another table also when that table can be without rows while the table has some, judged by following the not-null primary references, `child` never counting as always present. A query source declares it.
- Every query source and archived if required rule runs against the database, and the columns the async jobs read exist and are text.

## 8. Things to figure out

### 8.1 Duplicate persons

`person.duplicate_of` references another person. It is a deprecated workaround feature and `NULL` in most cases, but some municipalities may have old data in it. A `person` row cannot be deleted while a duplicate row references it, and we may also need to combine their data when evaluating the expiration rules. Until that is decided, the algorithm refuses to process a person whose row is a duplicate of another, or that has duplicates: the job aborts before loading anything.

### 8.2 Which rows are to be archived

The archived if required rule needs to know which rows must be archived before deletion. For `child_document` the schema says it: the template has an `archive_externally` flag and a trigger refuses to delete an unarchived document. For `decision`, `fee_decision` and `voucher_value_decision` there is only the `archived_at` column, and the knowledge of which rows get archived lives in the municipality-specific archival job, together with the fact that archival is enabled per municipality. If the rule treated every row as "to be archived", the nodes would never expire in a municipality without an archive, and rows that are never archived would be blocked forever.

### 8.3 Finance freeze

Fee decisions can be regenerated for any past period, so the family and income data they read, the parentships, partnerships and incomes, placements and service needs, etc. cannot be deleted while a regeneration could still need them. Once the *finance freeze* is implemented, finance decisions will not be generated further back in time than some fixed period. Until then, the rules of these tables are gated to never expire. Once it exists, the rules require the row's period to be older than the freeze, in addition to possible other expiration rules.

### 8.4 Anonymising orphan evaka_user rows

Whether the `name` column of an orphaned `evaka_user` row should be anonymised, for example to "Poistettu kuntalainen", is an open question.

### 8.5 Self-references between independent rows

`invoice.replaced_invoice_id` references the invoice that a corrected invoice replaced. As long as `invoice` is evaluated as a whole, the rows are deleted together and the reference does not matter. If invoices need `independentRows`, a replacing invoice must be deleted before the one it replaced. The easiest solution would then probably be `ON DELETE SET NULL`.

### 8.6 Voucher value report snapshots

The frozen monthly service voucher report is one `voucher_value_report_snapshot` row per month and its `voucher_value_report_decision` rows, each naming a voucher value decision. The algorithm waits for those rows before it deletes the decision but must not delete them (section 5.2), and nothing deletes them today. A job must delete each snapshot a set time after the month it covers, no longer than the ten years of the decisions themselves, or the decisions wait for the report. For the report rows to be deleted with their snapshot, `voucher_value_report_decision.voucher_value_report_snapshot_id` must become `ON DELETE CASCADE`; `decision_id` stays as it is, so that the rows keep holding the decision.

## 9. Existing issues to fix

### 9.1 Foreign keys to person that mean a child

Some foreign keys that name a person in the role of a child target `person` instead of `child` (section 1.1), such as `fee_decision_child.child_id` and `varda_state.child_id`. Migrating them to `child` and creating the rows where missing would let us remove some exceptions.
