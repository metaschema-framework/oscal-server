import React from 'react';
import { IonSelect, IonSelectOption } from '@ionic/react';
import { WidgetProps } from '@rjsf/utils';
import { useOscal } from '../../context/OscalContext';

const IonPartySelectWidget: React.FC<WidgetProps> = ({
  id,
  value,
  required,
  disabled,
  readonly,
  onChange,
  placeholder,
}) => {
  const oscal = useOscal();
  const parties = oscal.all('party') || {};

  const handleChange = (event: CustomEvent) => {
    const newValue = event.detail.value;
    onChange(newValue.length === 0 ? undefined : newValue);
  };

  return (
    <IonSelect
      id={id}
      value={value || []}
      placeholder={placeholder || "Select parties"}
      onIonChange={handleChange}
      disabled={disabled || readonly}
      interface="popover"
      className="w-full"
      multiple={true}
    >
      {Object.values(parties).map((party) => (
        <IonSelectOption key={party.uuid} value={party.uuid}>
          {party.name}
        </IonSelectOption>
      ))}
    </IonSelect>
  );
};

export default IonPartySelectWidget;
