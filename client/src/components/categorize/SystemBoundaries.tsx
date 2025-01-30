import React from "react";
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle } from "@ionic/react";

interface SystemBoundariesProps {
  description?: string;
}

const SystemBoundaries: React.FC<SystemBoundariesProps> = ({ description = "Define and document the system boundaries and interfaces" }) => {
  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>SystemBoundaries</IonCardTitle>
      </IonCardHeader>
      <IonCardContent>
        <p>{description}</p>
        {/* Add your component-specific content here */}
      </IonCardContent>
    </IonCard>
  );
};

export default SystemBoundaries;
