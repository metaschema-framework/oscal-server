import React, { useState } from 'react';
import { IonSelect, IonSelectOption, IonButton, IonItem, IonLabel, IonInput, IonList, IonIcon } from '@ionic/react';
import { arrowBack, close } from 'ionicons/icons';
import { WidgetProps } from '@rjsf/utils';
import { useOscal } from '../../context/OscalContext';

const commonBaselines = [
  {
    name: 'NIST SP 800-53 Low Impact',
    href: 'https://raw.githubusercontent.com/usnistgov/oscal-content/main/nist.gov/SP800-53/rev5/json/NIST_SP-800-53_rev5_LOW-baseline_profile.json'
  },
  {
    name: 'NIST SP 800-53 Moderate Impact',
    href: 'https://raw.githubusercontent.com/usnistgov/oscal-content/main/nist.gov/SP800-53/rev5/json/NIST_SP-800-53_rev5_MODERATE-baseline_profile.json'
  },
  {
    name: 'NIST SP 800-53 High Impact',
    href: 'https://raw.githubusercontent.com/usnistgov/oscal-content/main/nist.gov/SP800-53/rev5/json/NIST_SP-800-53_rev5_HIGH-baseline_profile.json'
  }
];

type ImportMethod = 'predefined' | 'url' | 'file' | null;

const IonImportProfileWidget: React.FC<WidgetProps> = ({
  id,
  value,
  onChange,
  readonly,
}) => {
  const [selectedMethod, setSelectedMethod] = useState<ImportMethod>(null);
  const [selectedBaseline, setSelectedBaseline] = useState('');
  const [url, setUrl] = useState('');
  const { insert } = useOscal();

  const handleFileUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    try {
      const text = await file.text();
      const profile = JSON.parse(text);
      await insert('profile', profile);
      onChange(file.name);
    } catch (error) {
      console.error('Failed to load profile:', error);
    }
  };

  const handleUrlImport = async () => {
    if (!url) return;
    try {
      const response = await fetch(url);
      const profile = await response.json();
      await insert('profile', profile);
      onChange(url);
      setUrl('');
    } catch (error) {
      console.error('Failed to load profile:', error);
    }
  };

  const handleBaselineSelect = async (event: CustomEvent) => {
    const href = event.detail.value;
    setSelectedBaseline(href);
    if (!href) return;

    try {
      const response = await fetch(href);
      const profile = await response.json();
      await insert('profile', profile);
      onChange(href);
    } catch (error) {
      console.error('Failed to load baseline:', error);
    }
  };

  const handleClear = () => {
    setSelectedMethod(null);
    setSelectedBaseline('');
    setUrl('');
    onChange(undefined);
  };

  const renderMethodSelection = () => (
    <IonList>
      <IonItem>
        <IonLabel>Select Import Method</IonLabel>
        <IonSelect
          value={selectedMethod}
          onIonChange={e => setSelectedMethod(e.detail.value)}
          interface="popover"
          disabled={readonly}
        >
          <IonSelectOption value="predefined">Use Predefined Baseline</IonSelectOption>
          <IonSelectOption value="url">Import from URL</IonSelectOption>
          <IonSelectOption value="file">Upload Profile File</IonSelectOption>
        </IonSelect>
      </IonItem>
    </IonList>
  );

  const renderImportInterface = () => {
    switch (selectedMethod) {
      case 'predefined':
        return (
          <IonItem>
            <IonLabel position="stacked">Select Common Baseline</IonLabel>
            <IonSelect
              value={selectedBaseline}
              onIonChange={handleBaselineSelect}
              interface="popover"
              disabled={readonly}
            >
              <IonSelectOption value="">Select a baseline...</IonSelectOption>
              {commonBaselines.map((baseline) => (
                <IonSelectOption key={baseline.href} value={baseline.href}>
                  {baseline.name}
                </IonSelectOption>
              ))}
            </IonSelect>
          </IonItem>
        );
      case 'url':
        return (
          <IonItem>
            <IonLabel position="stacked">Import from URL</IonLabel>
            <IonInput
              value={url}
              onIonChange={e => setUrl(e.detail.value || '')}
              placeholder="Enter profile URL..."
              disabled={readonly}
            />
            <IonButton 
              slot="end"
              onClick={handleUrlImport}
              disabled={!url || readonly}
            >
              Import
            </IonButton>
          </IonItem>
        );
      case 'file':
        return (
          <IonItem>
            <IonLabel position="stacked">Upload Profile File</IonLabel>
            <input
              type="file"
              accept="application/json"
              onChange={handleFileUpload}
              disabled={readonly}
              style={{ width: '100%', padding: '8px' }}
            />
          </IonItem>
        );
      default:
        return null;
    }
  };

  return (
    <div className="ion-padding">
      {selectedMethod ? (
        <>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
            <IonButton
              fill="clear"
              onClick={() => setSelectedMethod(null)}
            >
              <IonIcon slot="start" icon={arrowBack} />
              Back
            </IonButton>
            <IonButton
              fill="clear"
              onClick={handleClear}
            >
              <IonIcon slot="start" icon={close} />
              Clear
            </IonButton>
          </div>
          {renderImportInterface()}
        </>
      ) : (
        renderMethodSelection()
      )}

      {value && (
        <IonItem>
          <IonLabel>
            <h2>Current Profile</h2>
            <p>{value}</p>
          </IonLabel>
        </IonItem>
      )}
    </div>
  );
};

export default IonImportProfileWidget;
