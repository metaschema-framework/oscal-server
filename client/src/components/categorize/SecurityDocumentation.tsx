import React from "react";
import { IonCard, IonCardContent, IonCardHeader, IonCardTitle } from "@ionic/react";

interface SecurityDocumentationProps {
  description?: string;
}

const SecurityDocumentation: React.FC<SecurityDocumentationProps> = ({ description = "Document categorization in security, privacy, and SCRM plans" }) => {
  return (
    <IonCard>
      <IonCardHeader>
        <IonCardTitle>SecurityDocumentation</IonCardTitle>
      </IonCardHeader>
      <IonCardContent>
        <p>{description}</p>
        {/* Add your component-specific content here */}
      </IonCardContent>
    </IonCard>
  );
};

export default SecurityDocumentation;
