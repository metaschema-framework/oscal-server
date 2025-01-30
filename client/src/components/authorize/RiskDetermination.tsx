import React from 'react';
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem, IonLabel } from '@ionic/react';

const RiskDetermination: React.FC = () => {
  const tasks = [
    'Analyze authorization package',
    'Make risk-based decision',
    'Document authorization decision'
  ];

  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>Risk Determination</IonCardTitle>
      </IonCardHeader>
      <IonCardContent>
        <IonList>
          {tasks.map((task, index) => (
            <IonItem key={index}>
              <IonLabel>{task}</IonLabel>
            </IonItem>
          ))}
        </IonList>
      </IonCardContent>
    </IonCard>
  );
};

export default RiskDetermination;
