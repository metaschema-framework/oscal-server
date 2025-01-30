import React from "react";
import {
  IonContent,
  IonLabel,
  IonList,
  IonItem,
  IonText,
} from "@ionic/react";
import { Role } from "../../types";
import OscalListEdit from "../widgets/OscalListEdit";

const RiskManagementRoles: React.FC = () => {
  const renderRolePreview = (role: Role) => (
    <IonLabel className="ion-text-wrap">
      <IonText>{role.title}</IonText>
    </IonLabel>
  );

  const renderRoleDetails = (role: Role) => (
    <IonList>
      <IonItem>
        <IonLabel>
          <h2>Title</h2>
          <p>{role.title}</p>
        </IonLabel>
      </IonItem>
      <IonItem>
        <IonLabel>
          <h2>ID</h2>
          <p>{role.id}</p>
        </IonLabel>
      </IonItem>
      {role["short-name"] && (
        <IonItem>
          <IonLabel>
            <h2>Short Name</h2>
            <p>{role["short-name"]}</p>
          </IonLabel>
        </IonItem>
      )}
      {role.description && (
        <IonItem>
          <IonLabel>
            <h2>Description</h2>
            <p>{role.description}</p>
          </IonLabel>
        </IonItem>
      )}
    </IonList>
  );

  return (
    <IonContent>

      <OscalListEdit
        type="role"
        title="Roles"
        renderItemPreview={renderRolePreview}
        renderItemDetails={renderRoleDetails}
      />
    </IonContent>
  );
};

export default RiskManagementRoles;
