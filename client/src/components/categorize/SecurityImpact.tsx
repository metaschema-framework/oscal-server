import React from "react";
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle } from "@ionic/react";

interface SecurityImpactProps {
  description?: string;
}

const SecurityImpact: React.FC<SecurityImpactProps> = ({ description = "Determine security impact levels for confidentiality, integrity, and availability" }) => {
  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>SecurityImpact</IonCardTitle>
      </IonCardHeader>
      <IonCardContent>
        <p>{description}</p>
        {/* Add your component-specific content here */}
      </IonCardContent>
    </IonCard>
  );
};

export default SecurityImpact;
