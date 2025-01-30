import React from "react";
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle } from "@ionic/react";

interface RiskStrategyProps {
  description?: string;
}

const RiskStrategy: React.FC<RiskStrategyProps> = ({ description = "Ensure categorization reflects organizational risk management strategy" }) => {
  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>RiskStrategy</IonCardTitle>
      </IonCardHeader>
      <IonCardContent>
        <p>{description}</p>
        {/* Add your component-specific content here */}
      </IonCardContent>
    </IonCard>
  );
};

export default RiskStrategy;
