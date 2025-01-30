import React from 'react';
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem, IonLabel } from '@ionic/react';

const OngoingAssessment: React.FC = () => {
  const tasks = [
    'Conduct ongoing assessments',
    'Analyze assessment results',
    'Update system documentation'
  ];

  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>Ongoing Assessment</IonCardTitle>
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

export default OngoingAssessment;
