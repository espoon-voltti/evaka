import React, { useEffect, useState } from 'react'
import ReactSelect from 'react-select'
import { Result, Success } from '@evaka/lib-common/src/api'
import { useRestApi } from '@evaka/lib-common/src/utils/useRestApi'
import { queryAutocomplete } from '~map/api'
import { MapAddress } from '~map/MapView'
import { useTranslation } from '~localization'

type Props = {
  selectedAddress: MapAddress | null
  setSelectedAddress: (address: MapAddress | null) => void
}

export default React.memo(function SearchInput({
  selectedAddress,
  setSelectedAddress
}: Props) {
  const t = useTranslation()
  const [inputString, setInputString] = useState('')

  const [addressOptions, setAddressOptions] = useState<Result<MapAddress[]>>(
    Success.of([])
  )
  const loadOptions = useRestApi(queryAutocomplete, setAddressOptions)
  useEffect(() => {
    if (inputString.length > 0) {
      loadOptions(inputString)
    } else {
      setAddressOptions(Success.of([]))
    }
  }, [inputString])

  return (
    <div>
      <ReactSelect
        isSearchable
        closeMenuOnSelect
        isLoading={addressOptions.isLoading}
        inputValue={inputString}
        onInputChange={(val: string) => setInputString(val)}
        options={addressOptions.isSuccess ? addressOptions.value : []}
        getOptionLabel={(option) =>
          `${option.streetAddress}, ${option.postOffice ?? ''}`
        }
        value={selectedAddress}
        onChange={(selected) => {
          if (!selected) {
            setSelectedAddress(null)
          } else if ('length' in selected) {
            setSelectedAddress(selected.length ? selected[0] : null)
          } else {
            setSelectedAddress(selected)
          }
        }}
        placeholder={t.map.searchPlaceholder}
      />
    </div>
  )
})
