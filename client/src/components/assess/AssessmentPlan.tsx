import React from 'react';
import { IonCard, IonContent, IonHeader, IonPage, IonTitle, IonToolbar, IonList, IonItem, IonLabel } from '@ionic/react';

const AssessmentPlan: React.FC = () => {
  const tasks = [
    'Define assessment scope',
    'Set assessment objectives',
    'Specify assessment methods',
    'Schedule assessment activities',
    'Assign assessment resources'
  ];

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonTitle>Assess - Assessment Plan</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent>
        <IonCard>
          <IonList>
            {tasks.map((task, index) => (
              <IonItem key={index}>
                <IonLabel>{task}</IonLabel>
              </IonItem>
            ))}
          </IonList>
        </IonCard>
      </IonContent>
    </IonPage>
  );
};

export default AssessmentPlan;
