import { IonContent, IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem } from '@ionic/react';

const ControlTailoring: React.FC = () => {
  return (
    <IonContent>
      <IonCard>
        <IonCardHeader>
          <IonCardTitle>S-2 Control Tailoring</IonCardTitle>
        </IonCardHeader>
        <IonCardContent>
          <IonList>
            <IonItem>
              Security controls are tailored based on system-specific conditions and organizational considerations.
            </IonItem>
            <IonItem>
              Control parameters are defined according to operational requirements.
            </IonItem>
            <IonItem>
              Tailoring decisions and rationale are documented.
            </IonItem>
          </IonList>
          [Cybersecurity Framework: Profile; PR.IP]
        </IonCardContent>
      </IonCard>
    </IonContent>
  );
};

export default ControlTailoring;
