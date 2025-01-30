import { IonContent, IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem } from '@ionic/react';

const ControlAllocation: React.FC = () => {
  return (
    <IonContent>
      <IonCard>
        <IonCardHeader>
          <IonCardTitle>S-3 Control Allocation</IonCardTitle>
        </IonCardHeader>
        <IonCardContent>
          <IonList>
            <IonItem>
              Controls are designated as system-specific, hybrid, or common controls.
            </IonItem>
            <IonItem>
              Controls are allocated to the specific system elements (i.e., machine, physical, or human elements).
            </IonItem>
          </IonList>
          [Cybersecurity Framework: Profile; PR.IP]
        </IonCardContent>
      </IonCard>
    </IonContent>
  );
};

export default ControlAllocation;
