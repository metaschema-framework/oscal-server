import React from 'react';
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem, IonLabel } from '@ionic/react';

const AssessmentPreparation: React.FC = () => {
  const tasks = [
    'Develop assessment plan',
    'Select assessment procedures',
    'Optimize assessment approach'
  ];

  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>Assessment Preparation</IonCardTitle>
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

export default AssessmentPreparation;
