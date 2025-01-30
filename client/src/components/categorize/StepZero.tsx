import React from "react";
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle } from "@ionic/react";

interface StepZeroProps {
  description?: string;
}

const StepZero: React.FC<StepZeroProps> = ({ description = "Obtain senior leadership approval for categorization decisions" }) => {
  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>Step 0</IonCardTitle>
      </IonCardHeader>
      <IonCardContent>
        <p>{description}</p>
        {/* Add your component-specific content here */}
      </IonCardContent>
    </IonCard>
  );
};

export default StepZero;
