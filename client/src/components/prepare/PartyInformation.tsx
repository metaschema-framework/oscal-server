import React from "react";
import {
  IonContent,
  IonIcon,
  IonLabel,
  IonButtons,
  IonText,
  IonList,
  IonItem,
} from "@ionic/react";
import { businessOutline, personOutline } from "ionicons/icons";
import { Party } from "../../types";
import OscalListEdit from "../widgets/OscalListEdit";

const PartyInformation: React.FC = () => {
  const renderPartyPreview = (party: Party) => (
    <>
      <IonButtons slot="start">
        <IonIcon icon={party.type === "person" ? personOutline : businessOutline} />
      </IonButtons>
      <IonLabel className="ion-text-wrap">
        <IonText>{party.name}</IonText>
      </IonLabel>
    </>
  );

  const renderPartyDetails = (party: Party) => (
    <IonList>
      <IonItem>
        <IonLabel>
          <h2>Name</h2>
          <p>{party.name}</p>
        </IonLabel>
      </IonItem>
      <IonItem>
        <IonLabel>
          <h2>Type</h2>
          <p>{party.type}</p>
        </IonLabel>
      </IonItem>
      {party["short-name"] && (
        <IonItem>
          <IonLabel>
            <h2>Short Name</h2>
            <p>{party["short-name"]}</p>
          </IonLabel>
        </IonItem>
      )}
      {party["email-addresses"] && party["email-addresses"].length > 0 && (
        <IonItem>
          <IonLabel>
            <h2>Email Addresses</h2>
            {party["email-addresses"].map((email, index) => (
              <p key={index}>{email}</p>
            ))}
          </IonLabel>
        </IonItem>
      )}
      {party.remarks && (
        <IonItem>
          <IonLabel>
            <h2>Remarks</h2>
            <p>{party.remarks}</p>
          </IonLabel>
        </IonItem>
      )}
    </IonList>
  );

  return (
    <IonContent>
      <OscalListEdit
        type="party"
        title="Parties"
        renderItemPreview={renderPartyPreview}
        renderItemDetails={renderPartyDetails}
      />
    </IonContent>
  );
};

export default PartyInformation;
