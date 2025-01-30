import React from 'react';
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem, IonLabel } from '@ionic/react';

const AuthorizationPackage: React.FC = () => {
  const tasks = [
    'Assemble authorization package',
    'Perform risk assessment',
    'Determine risk responses'
  ];

  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>Authorization Package</IonCardTitle>
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

export default AuthorizationPackage;
