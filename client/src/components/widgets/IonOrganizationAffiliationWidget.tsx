import React from 'react';
import { IonSelect, IonSelectOption } from '@ionic/react';
import { WidgetProps } from '@rjsf/utils';
import { useOscal } from '../../context/OscalContext';

const IonOrganizationAffiliationWidget: React.FC<WidgetProps> = ({
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

  // Filter parties that are organizations
  const organizations = Object.values(parties).filter(
    party => party.type === 'organization'
  );

  const handleChange = (event: CustomEvent) => {
    const newValue = event.detail.value;
    onChange(newValue.length === 0 ? undefined : newValue);
  };

  return (
    <IonSelect
      id={id}
      value={value || []}
      placeholder={placeholder || "Select organizations"}
      onIonChange={handleChange}
      disabled={disabled || readonly}
      interface="popover"
      className="w-full"
      multiple={true}
    >
      {organizations.map((org) => (
        <IonSelectOption key={org.uuid} value={org.uuid}>
          {org.name}
        </IonSelectOption>
      ))}
    </IonSelect>
  );
};

export default IonOrganizationAffiliationWidget;
