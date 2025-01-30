import { IonContent, IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem } from '@ionic/react';

const ControlDocumentation: React.FC = () => {
  return (
    <IonContent>
      <IonCard>
        <IonCardHeader>
          <IonCardTitle>S-4 Control Documentation</IonCardTitle>
        </IonCardHeader>
        <IonCardContent>
          <IonList>
            <IonItem>
              Security control implementation details are documented.
            </IonItem>
            <IonItem>
              Control implementation descriptions include planned inputs, expected behavior, and expected outputs.
            </IonItem>
            <IonItem>
              Documentation includes functional properties, security and privacy characteristics, and implementation details.
            </IonItem>
          </IonList>
          [Cybersecurity Framework: Profile; PR.IP]
        </IonCardContent>
      </IonCard>
    </IonContent>
  );
};

export default ControlDocumentation;
