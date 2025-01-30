import React from 'react';
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem, IonLabel } from '@ionic/react';

const SystemDescription: React.FC = () => {
  const tasks = [
    'Document system characteristics',
    'Define system boundary',
    'Register the system'
  ];

  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>System Description</IonCardTitle>
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

export default SystemDescription;
