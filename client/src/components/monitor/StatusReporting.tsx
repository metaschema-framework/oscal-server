import React from 'react';
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem, IonLabel } from '@ionic/react';

const StatusReporting: React.FC = () => {
  const tasks = [
    'Report security status',
    'Track POA&M items',
    'Update risk assessment'
  ];

  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>Status Reporting</IonCardTitle>
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

export default StatusReporting;
