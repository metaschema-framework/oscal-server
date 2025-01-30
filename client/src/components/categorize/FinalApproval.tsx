import React from "react";
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle } from "@ionic/react";

interface FinalApprovalProps {
  description?: string;
}

const FinalApproval: React.FC<FinalApprovalProps> = ({ description = "Obtain senior leadership approval for categorization decisions" }) => {
  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>FinalApproval</IonCardTitle>
      </IonCardHeader>
      <IonCardContent>
        <p>{description}</p>
        {/* Add your component-specific content here */}
      </IonCardContent>
    </IonCard>
  );
};

export default FinalApproval;
