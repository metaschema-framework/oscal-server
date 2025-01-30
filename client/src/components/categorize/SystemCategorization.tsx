import React from 'react';
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem, IonLabel } from '@ionic/react';

const SystemCategorization: React.FC = () => {
  const tasks = [
    'Define security categorization',
    'Obtain senior leadership approval',
    'Document categorization decisions'
  ];

  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>System Categorization</IonCardTitle>
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

export default SystemCategorization;
