import React from 'react';
import { IonSelect, IonSelectOption, IonItem, IonLabel } from '@ionic/react';
import { WidgetProps } from '@rjsf/utils';
import { useOscal } from '../../context/OscalContext';

interface ResponsibleParty {
  'role-id': string;
  'party-uuids': string[];
}

const IonResponsiblePartyWidget: React.FC<WidgetProps> = ({
  id,
  value,
  required,
  disabled,
  readonly,
  onChange,
  schema,
  options,
}) => {
  const oscal = useOscal();
  const parties = oscal.all('party') || {};
  const roles = oscal.all('role') || {};

  // Get current responsible party data
  const currentValue = value as ResponsibleParty;
  
  // Filter parties based on type if specified in schema
  const partyType = schema?.partyType as string | undefined;
  const filteredParties = Object.values(parties).filter(party => 
    !partyType || party.type === partyType
  );

  const handlePartyChange = (event: CustomEvent) => {
    const partyUuids = event.detail.value;
    onChange({
      'role-id': currentValue?.['role-id'] || schema?.roleId || '',
      'party-uuids': partyUuids
    });
  };

  const handleRoleChange = (event: CustomEvent) => {
    const roleId = event.detail.value;
    onChange({
      'role-id': roleId,
      'party-uuids': currentValue?.['party-uuids'] || []
    });
  };

  return (
    <div>
      {!schema?.roleId && (
        <IonItem>
          <IonLabel position="stacked">Role</IonLabel>
          <IonSelect
            value={currentValue?.['role-id'] || ''}
            placeholder="Select role..."
            onIonChange={handleRoleChange}
            disabled={disabled || readonly}
            interface="popover"
          >
            {Object.values(roles).map((role) => (
              <IonSelectOption key={role.id} value={role.id}>
                {role.title}
              </IonSelectOption>
            ))}
          </IonSelect>
        </IonItem>
      )}

      <IonItem>
        <IonLabel position="stacked">Responsible {partyType || 'Parties'}</IonLabel>
        <IonSelect
          value={currentValue?.['party-uuids'] || []}
          placeholder={`Select ${partyType || 'parties'}...`}
          onIonChange={handlePartyChange}
          disabled={disabled || readonly}
          interface="popover"
          multiple={true}
        >
          {filteredParties.map((party) => (
            <IonSelectOption key={party.uuid} value={party.uuid}>
              {party.name}
            </IonSelectOption>
          ))}
        </IonSelect>
      </IonItem>
    </div>
  );
};

export default IonResponsiblePartyWidget;
