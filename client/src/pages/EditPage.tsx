import React from 'react';
import { 
  IonContent, 
  IonHeader, 
  IonPage, 
  IonTitle, 
  IonToolbar,
  IonButtons,
  IonBackButton,
  IonText
} from '@ionic/react';
import { useParams } from 'react-router';
import OscalForm from '../components/OscalForm';

const EditPage: React.FC = () => {
  const { type } = useParams<{ type: string }>();
  console.log('EditPage rendered with type:', type);

  const handleSubmit = (data: Record<string, unknown>) => {
    console.log('Form submitted:', data);
    // TODO: Implement form submission logic
  };

  return (
    <IonPage>
      <IonHeader>
        <IonToolbar>
          <IonButtons slot="start">
            <IonBackButton defaultHref="/prepare" />
          </IonButtons>
          <IonTitle>Edit {type}</IonTitle>
        </IonToolbar>
      </IonHeader>
      <IonContent className="ion-padding">
        {type ? (
          <OscalForm type={type} onSubmit={handleSubmit} />
        ) : (
          <div className="ion-text-center">
            <IonText color="danger">
              <p>Invalid type parameter</p>
            </IonText>
          </div>
        )}
      </IonContent>
    </IonPage>
  );
};

export default EditPage;
