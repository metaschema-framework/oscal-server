import { IonContent, IonPage } from '@ionic/react';
import PageHeader from '../components/common/PageHeader';
import ControlSelection from '../components/select/ControlSelection';
import ControlTailoring from '../components/select/ControlTailoring';
import ControlAllocation from '../components/select/ControlAllocation';
import ControlDocumentation from '../components/select/ControlDocumentation';
import MonitoringStrategy from '../components/select/MonitoringStrategy';
import PlanReview from '../components/select/PlanReview';
import SubMenu from '../components/SubMenu';

const Select: React.FC = () => {
  return (
    <IonPage>
      <PageHeader base='select' title="Select Phase" />
      <IonContent>
        <SubMenu routes={[
          { component: ControlSelection, slug: 's1-control-selection', title: 'S-1 Control Selection' },
          { component: ControlTailoring, slug: 's2-control-tailoring', title: 'S-2 Control Tailoring' },
          { component: ControlAllocation, slug: 's3-control-allocation', title: 'S-3 Control Allocation' },
          { component: ControlDocumentation, slug: 's4-control-documentation', title: 'S-4 Control Documentation' },
          { component: MonitoringStrategy, slug: 's-5-monitoring-strategy', title: 'S-5 Monitoring Strategy' },
          { component: PlanReview, slug: 's-6-plan-review', title: 'S-6 Plan Review' }
        ]}/>
      </IonContent>
    </IonPage>
  );
};

export default Select;
