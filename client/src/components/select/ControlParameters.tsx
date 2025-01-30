import React from 'react';
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem, IonLabel } from '@ionic/react';

const ControlParameters: React.FC = () => {
  const tasks = [
    'Assign specific parameter values',
    'Document parameter value assignments'
  ];

  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>Control Parameters</IonCardTitle>
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

export default ControlParameters;
