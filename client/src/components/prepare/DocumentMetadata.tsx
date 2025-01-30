import React from 'react';
import { IonCard, IonContent, IonHeader, IonPage, IonTitle, IonToolbar, IonList, IonItem, IonLabel } from '@ionic/react';

const DocumentMetadata: React.FC = () => {
  const tasks = [
    'Define document title and version',
    'Set publication date and status',
    'Add document identifiers',
    'Specify document properties',
    'Manage revision history'
  ];

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonTitle>Prepare - Document Metadata</IonTitle>
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

export default DocumentMetadata;
