import React from 'react';
import { IonContent, IonPage } from '@ionic/react';
import ExploreContainer from '../components/ExploreContainer';
import SubMenu from '../components/SubMenu';
import AssessmentPreparation from '../components/assess/AssessmentPreparation';
import SecurityControlAssessment from '../components/assess/SecurityControlAssessment';
import AssessmentDocumentation from '../components/assess/AssessmentDocumentation';
import AssessmentPlan from '../components/assess/AssessmentPlan';
import PageHeader from '../components/common/PageHeader';

const Assess: React.FC = () => {
  return (
    <IonPage>
      <PageHeader base='assess' title="Assess Phase" />
      <IonContent fullscreen>
        <SubMenu routes={[
          { component: AssessmentPreparation, slug: 'assessment-preparation' },
          { component: SecurityControlAssessment, slug: 'security-control-assessment' },
          { component: AssessmentDocumentation, slug: 'assessment-documentation' },
          { component: AssessmentPlan, slug: 'assessment-plan' }
        ]}/>
      </IonContent>
    </IonPage>
  );
};

export default Assess;
