import React from 'react';
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem, IonLabel } from '@ionic/react';

const SupplementalControls: React.FC = () => {
  const tasks = [
    'Select compensating controls if needed',
    'Document supplemental control decisions'
  ];

  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>Supplemental Controls</IonCardTitle>
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

export default SupplementalControls;
