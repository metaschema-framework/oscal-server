import React from 'react';
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem, IonLabel } from '@ionic/react';

const SystemEnvironmentChanges: React.FC = () => {
  const tasks = [
    'Monitor security controls',
    'Assess control effectiveness',
    'Document changes'
  ];

  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>System and Environment Changes</IonCardTitle>
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

export default SystemEnvironmentChanges;
