import React from 'react';
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem, IonLabel } from '@ionic/react';

const ControlDocumentation: React.FC = () => {
  const tasks = [
    'Update system security plan',
    'Document control implementation details',
    'Create implementation evidence'
  ];

  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>Control Documentation</IonCardTitle>
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

export default ControlDocumentation;
