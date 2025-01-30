import React from "react";
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle } from "@ionic/react";

interface SystemCharacteristicsProps {
  description?: string;
}

const SystemCharacteristics: React.FC<SystemCharacteristicsProps> = ({ description = "Document and describe the characteristics of the system" }) => {
  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>SystemCharacteristics</IonCardTitle>
      </IonCardHeader>
      <IonCardContent>
        <p>{description}</p>
        {/* Add your component-specific content here */}
      </IonCardContent>
    </IonCard>
  );
};

export default SystemCharacteristics;
