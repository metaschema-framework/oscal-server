import React from 'react';
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem, IonLabel } from '@ionic/react';

const AssessmentDocumentation: React.FC = () => {
  const tasks = [
    'Create security assessment report',
    'Update security plan',
    'Create plan of action and milestones'
  ];

  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>Assessment Documentation</IonCardTitle>
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

export default AssessmentDocumentation;
