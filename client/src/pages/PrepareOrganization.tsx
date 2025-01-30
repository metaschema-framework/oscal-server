import { IonButton, IonCard, IonCardContent, IonCardHeader, IonContent, IonPage } from "@ionic/react";
import PageHeader from "../components/common/PageHeader";
import SubMenu from "../components/SubMenu";
import RiskManagementRoles from "../components/prepare/RiskManagementRoles";
import RiskManagementStrategy from "../components/prepare/RiskManagementStrategy";
import PartyInformation from "../components/prepare/PartyInformation";
import OrganizationRiskAssessment from "../components/prepare/OrganizationRiskAssessment";
import TailoredControlBaselines from "../components/prepare/TailoredControlBaselines";
import CommonControlIdentification from "../components/prepare/CommonControlIdentification";
import ImpactLevelPrioritization from "../components/prepare/ImpactLevelPrioritization";
import ContinuousMonitoringStrategy from "../components/prepare/ContinuousMonitoringStrategy";
import { PrepareOrganizationInfo } from "./PrepareOrganizationInfo";

const PrepareOrganization: React.FC = () => {
  return (
    <IonPage>
      <PageHeader base="prepare" title="Prepare Organization" />
      <IonContent>
        
        <SubMenu
          routes={[
            {
              component: PartyInformation,
              slug: "p1-parties",
            },
            {
              component: RiskManagementRoles,
              slug: "p2-roles",
            },
            {
              component: RiskManagementStrategy,
              slug: "p2-strategy",
            },
            {
              component: OrganizationRiskAssessment,
              slug: "p3-organization",
            },
            {
              component: TailoredControlBaselines,
              slug: "p4-tailoring",
            },
            {
              component: CommonControlIdentification,
              slug: "p5-common-controls",
            },
            {
              component: ImpactLevelPrioritization,
              slug: "p6-impact-level",
            },
            {
              component: ContinuousMonitoringStrategy,
              slug: "p7-continuous-monitoring",
            },
          ]}
        />
      </IonContent>
    </IonPage>
  );
};

export default PrepareOrganization;
