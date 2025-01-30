import React from 'react';
import { IonCard, IonContent, IonHeader, IonPage, IonTitle, IonToolbar, IonList, IonItem, IonLabel } from '@ionic/react';

const ControlGroups: React.FC = () => {
  const tasks = [
    'Create control groups',
    'Organize controls by family',
    'Set group properties',
    'Define group relationships',
    'Document group metadata'
  ];

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonTitle>Categorize - Control Groups</IonTitle>
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

export default ControlGroups;
