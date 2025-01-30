import { IonButton, IonIcon, IonLabel, IonProgressBar } from '@ionic/react';
import { checkmarkCircle } from 'ionicons/icons';
import { useCompletion } from '../context/CompletionContext';

const StepLink = ({ base = "/", slug = "", label = "" }) => {
  const isSubStep = slug;
  const completion = useCompletion(x => x.stepCompletion[slug || base]) ?? 0;
  const isComplete = completion === 100;

  // Ensure base has trailing slash
  const normalizedBase = base.endsWith('/') ? base : `${base}/`;
  
  if (isSubStep) {
    return (
      <IonButton 
        routerLink={normalizedBase}
        color={isComplete ? 'success' : 'primary'}
        style={{ marginBottom: '1rem', width: '100%' }}
      >
        <IonLabel>{label}</IonLabel>
        {isComplete && <IonIcon icon={checkmarkCircle} slot="end" />}
      </IonButton>
    );
  }

  return (
    <IonButton 
      routerLink={`${normalizedBase}${slug}`}
      fill='clear'
    >
      <IonLabel>
        <h2 style={{ fontWeight: 600 }}>{label}</h2>
        <IonProgressBar
          value={completion / 100}
          style={{ marginTop: '0.5rem' }}
          color="primary"
        />
        <div style={{ 
          textAlign: 'right',
          fontSize: '0.875rem',
          marginTop: '0.25rem',
          color: '#4b5563'
        }}>
          {Math.round(completion)}%
        </div>
      </IonLabel>
    </IonButton>
  );
};

export default StepLink;