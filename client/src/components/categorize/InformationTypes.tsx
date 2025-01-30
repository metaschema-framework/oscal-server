import React from "react";
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle } from "@ionic/react";

interface InformationTypesProps {
  description?: string;
}

const InformationTypes: React.FC<InformationTypesProps> = ({ description = "Identify and categorize information types processed by the system" }) => {
  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>InformationTypes</IonCardTitle>
      </IonCardHeader>
      <IonCardContent>
        <p>{description}</p>
        {/* Add your component-specific content here */}
      </IonCardContent>
    </IonCard>
  );
};

export default InformationTypes;
