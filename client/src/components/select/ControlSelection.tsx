import { IonContent, IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem } from '@ionic/react';

const ControlSelection: React.FC = () => {
  return (
    <IonContent>
      <IonCard>
        <IonCardHeader>
          <IonCardTitle>S-1 Control Selection</IonCardTitle>
        </IonCardHeader>
        <IonCardContent>
          <IonList>
            <IonItem>
              Initial security control baseline is selected based on the security categorization.
            </IonItem>
            <IonItem>
              Security controls are selected to adequately protect the system.
            </IonItem>
            <IonItem>
              Control selection rationale is documented.
            </IonItem>
          </IonList>
          [Cybersecurity Framework: Profile; ID.GV, PR.IP]
        </IonCardContent>
      </IonCard>
    </IonContent>
  );
};

export default ControlSelection;
