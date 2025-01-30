import { IonContent, IonPage } from '@ionic/react';
import PageHeader from '../components/common/PageHeader';
import SubMenu from '../components/SubMenu';
import MissionBusinessFocus from '../components/prepare/MissionBusinessFocus';
import SystemStakeholders from '../components/prepare/SystemStakeholders';
import AssetIdentification from '../components/prepare/AssetIdentification';
import AuthorizationBoundary from '../components/prepare/AuthorizationBoundary';
import InformationTypes from '../components/prepare/InformationTypes';
import InformationLifecycle from '../components/prepare/InformationLifecycle';
import SystemRiskAssessment from '../components/prepare/SystemRiskAssessment';
import RequirementsDefinition from '../components/prepare/RequirementsDefinition';
import EnterpriseArchitecture from '../components/prepare/EnterpriseArchitecture';
import RequirementsAllocation from '../components/prepare/RequirementsAllocation';
import SystemRegistration from '../components/prepare/SystemRegistration';

const PrepareSystem: React.FC = () => {
  return (
    <IonPage>
      <PageHeader base='prepare' title="Prepare System" />
      <IonContent>
        <SubMenu routes={[
          { 
            component: MissionBusinessFocus,
            slug: 'p8-mission-focus',
          },
          { 
            component: SystemStakeholders,
            slug: 'p9-stakeholders',
          },
          { 
            component: AssetIdentification,
            slug: 'p10-assets',
          },
          { 
            component: AuthorizationBoundary,
            slug: 'p11-authorization',
          },
          { 
            component: InformationTypes,
            slug: 'p12-info-types',
          },
          { 
            component: InformationLifecycle,
            slug: 'p13-info-lifecycle',
          },
          { 
            component: SystemRiskAssessment,
            slug: 'p14-risk-assessment',
          },
          { 
            component: RequirementsDefinition,
            slug: 'p15-requirements',
          },
          { 
            component: EnterpriseArchitecture,
            slug: 'p16-enterprise-arch',
          },
          { 
            component: RequirementsAllocation,
            slug: 'p17-req-allocation',
          },
          { 
            component: SystemRegistration,
            slug: 'p18-registration',
          }
        ]}/>
      </IonContent>
    </IonPage>
  );
};

export default PrepareSystem;