import React, { useEffect, useState } from "react";
import { IonContent, IonCard, IonCardHeader, IonCardTitle, IonCardContent } from "@ionic/react";
import { useOscal } from "../../context/OscalContext";
import OscalForm from "../OscalForm";

const TailoredControlBaselines: React.FC = () => {
  const { insert, read } = useOscal();
  const [ssp, setSsp] = useState<any>({});

  useEffect(() => {
    const loadSsp = async () => {
      const data = await read('system-security-plan', 'default');
      setSsp(data || {});
    };
    loadSsp();
  }, [read]);

  return (
    <IonContent>
      <IonCard>
        <IonCardHeader>
          <IonCardTitle>Control Baseline</IonCardTitle>
        </IonCardHeader>
        <IonCardContent>
          <OscalForm
            type="system-security-plan"
            initialData={{
              'import-profile': {
                href: ssp['import-profile']?.href
              }
            }}
            onSubmit={(data) => {
              // Update only the import-profile part of the SSP
              insert('system-security-plan', {
                ...ssp,
                'import-profile': data['import-profile']
              });
            }}
          />
        </IonCardContent>
      </IonCard>
    </IonContent>
  );
};

export default TailoredControlBaselines;
