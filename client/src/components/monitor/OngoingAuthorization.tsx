import React from 'react';
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem, IonLabel } from '@ionic/react';

const OngoingAuthorization: React.FC = () => {
  const tasks = [
    'Review authorization decision',
    'Update authorization package',
    'Implement system decommissioning strategy when needed'
  ];

  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>Ongoing Authorization</IonCardTitle>
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

export default OngoingAuthorization;
