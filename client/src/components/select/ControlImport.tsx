import React from 'react';
import { IonCard, IonContent, IonHeader, IonPage, IonTitle, IonToolbar, IonList, IonItem, IonLabel } from '@ionic/react';

const ControlImport: React.FC = () => {
  const tasks = [
    'Import control catalog',
    'Select control baseline',
    'Review imported controls',
    'Validate control definitions',
    'Map control relationships'
  ];

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonTitle>Select - Control Import</IonTitle>
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

export default ControlImport;
