import React from 'react';
import {
  IonContent,
  IonItem,
  IonLabel,
  IonButton,
  IonList,
  IonIcon,
  IonGrid,
  IonRow,
  IonCol,
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardContent,
  IonText,
  IonChip,
} from '@ionic/react';
import { trash } from 'ionicons/icons';
import { useOscal } from '../../context/OscalContext';
import OscalForm from '../OscalForm';

const OrganizationRiskAssessment: React.FC = () => {
  const { insert, all, destroy } = useOscal();
  const assessments = all('risk-assessment') || {};

  const handleDelete = async (uuid: string) => {
    await destroy('risk-assessment', uuid);
  };

  const renderAssessmentDetails = (assessment: any) => (
    <IonItem key={assessment.uuid}>
      <IonLabel className="ion-text-wrap">
        {assessment.threats?.length > 0 && (
          <div className="ion-padding-vertical">
            <h3>Threats</h3>
            {assessment.threats.map((threat: string, index: number) => (
              <IonChip key={index}>{threat}</IonChip>
            ))}
          </div>
        )}
        {assessment.vulnerabilities?.length > 0 && (
          <div className="ion-padding-vertical">
            <h3>Vulnerabilities</h3>
            {assessment.vulnerabilities.map((vulnerability: string, index: number) => (
              <IonChip key={index}>{vulnerability}</IonChip>
            ))}
          </div>
        )}
        {assessment.impactAssessment && (
          <div className="ion-padding-vertical">
            <h3>Impact Assessment</h3>
            <p>{assessment.impactAssessment}</p>
          </div>
        )}
        {assessment.riskDetermination && (
          <div className="ion-padding-vertical">
            <h3>Risk Determination</h3>
            <p>{assessment.riskDetermination}</p>
          </div>
        )}
      </IonLabel>
      <IonButton 
        fill="clear" 
        slot="end"
        onClick={() => handleDelete(assessment.uuid)}
      >
        <IonIcon icon={trash} />
      </IonButton>
    </IonItem>
  );

  return (
    <IonContent>
      <IonGrid>
        <IonRow>
          <IonCol size="4">
            <IonCard>
              <IonCardHeader>
                <IonCardTitle>Risk Assessments</IonCardTitle>
              </IonCardHeader>
              <IonCardContent>
                <IonList>
                  {Object.values(assessments).length > 0 ? (
                    Object.values(assessments).map(renderAssessmentDetails)
                  ) : (
                    <IonItem>
                      <IonLabel>
                        <IonText color="medium">
                          No risk assessments defined yet.
                        </IonText>
                      </IonLabel>
                    </IonItem>
                  )}
                </IonList>
              </IonCardContent>
            </IonCard>
          </IonCol>
          <IonCol size="8">
            <OscalForm 
              type="risk-assessment" 
              onSubmit={(assessment) => insert('risk-assessment', assessment)}
            />
          </IonCol>
        </IonRow>
      </IonGrid>
    </IonContent>
  );
};

export default OrganizationRiskAssessment;
