import React from "react";
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle } from "@ionic/react";

interface SeniorReviewProps {
  description?: string;
}

const SeniorReview: React.FC<SeniorReviewProps> = ({ description = "Review of security categorization results by senior leadership" }) => {
  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>SeniorReview</IonCardTitle>
      </IonCardHeader>
      <IonCardContent>
        <p>{description}</p>
        {/* Add your component-specific content here */}
      </IonCardContent>
    </IonCard>
  );
};

export default SeniorReview;
