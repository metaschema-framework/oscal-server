import { IonContent, IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem } from '@ionic/react';

const SecurityControlSelection: React.FC = () => {
  return (
    <IonContent>
      <IonCard>
        <IonCardHeader>
          <IonCardTitle>Security Control Selection Process</IonCardTitle>
        </IonCardHeader>
        <IonCardContent>
          <IonList>
            <IonItem>
              Baseline controls are selected based on system categorization and impact levels.
            </IonItem>
            <IonItem>
              Controls are tailored according to organizational requirements and system-specific needs.
            </IonItem>
            <IonItem>
              Additional controls are selected based on risk assessment results and specific threats.
            </IonItem>
            <IonItem>
              Control selection decisions and rationale are documented in the security plan.
            </IonItem>
          </IonList>
          [Cybersecurity Framework: Profile; ID.RA, PR.IP]
        </IonCardContent>
      </IonCard>
    </IonContent>
  );
};

export default SecurityControlSelection;
