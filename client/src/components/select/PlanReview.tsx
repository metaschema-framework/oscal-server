import { IonContent, IonCard, IonCardContent, IonCardHeader, IonCardTitle, IonList, IonItem } from '@ionic/react';

const PlanReview: React.FC = () => {
  return (
    <IonContent>
      <IonCard>
        <IonCardHeader>
          <IonCardTitle>S-6 Plan Review and Approval</IonCardTitle>
        </IonCardHeader>
        <IonCardContent>
          <IonList>
            <IonItem>
              Security and privacy plans are reviewed to ensure completeness, consistency, and compliance.
            </IonItem>
            <IonItem>
              Plans are updated based on review findings and feedback.
            </IonItem>
            <IonItem>
              Final security and privacy plans are approved by authorizing official or designated representative.
            </IonItem>
          </IonList>
          [Cybersecurity Framework: Profile; ID.GV, PR.IP]
        </IonCardContent>
      </IonCard>
    </IonContent>
  );
};

export default PlanReview;
