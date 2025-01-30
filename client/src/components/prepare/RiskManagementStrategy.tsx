import React from 'react';
import {
  IonContent,
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardContent,
} from '@ionic/react';
import { useOscal } from '../../context/OscalContext';
import OscalForm from '../OscalForm';
import { RJSFSchema } from '@rjsf/utils';

interface RiskManagementData {
  riskAssumptions: string;
  riskConstraints: string;
  riskManagementRoles: string[];
  riskTolerance: string;
  controlImplementation: string;
  documentReference: string;
}

interface Property {
  name: string;
  value: string;
}

interface ResponsibleParty {
  'role-id': string;
  'party-uuids': string[];
}

const RiskManagementStrategy: React.FC = () => {
  const { insert, read } = useOscal();

  const handleSubmit = async (formData: Record<string, unknown>) => {
    await insert('risk-management-strategy', formData);
  };

  return (
    <IonContent>
      <IonCard>
        <IonCardHeader>
          <IonCardTitle>Risk Management Strategy</IonCardTitle>
        </IonCardHeader>
        <IonCardContent>
          <OscalForm 
            type="risk-management-strategy"
            onSubmit={handleSubmit}
          />
        </IonCardContent>
      </IonCard>
    </IonContent>
  );
};

export default RiskManagementStrategy;
