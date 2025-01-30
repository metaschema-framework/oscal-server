import React, { useState } from 'react';
import { IonItem, IonLabel, IonInput, IonButton, IonList, IonText } from '@ionic/react';
import { WidgetProps } from '@rjsf/utils';
import { useOscal } from '../../context/OscalContext';

const IonDocumentResourceWidget: React.FC<WidgetProps> = ({
  id,
  value,
  onChange,
  readonly,
}) => {
  const [title, setTitle] = useState('');
  const [href, setHref] = useState('');
  const { insert, read } = useOscal();

  const handleAdd = async () => {
    if (!title || !href) return;

    const ssp = await read('system-security-plan', 'default') || {};
    const backMatter = ssp.back_matter || {};
    const resources = backMatter.resources || [];

    const resource = {
      uuid: crypto.randomUUID(),
      title,
      description: 'Risk Management Strategy Document',
      rlinks: [{
        href,
        media_type: 'application/pdf'
      }]
    };

    const updatedSSP = {
      ...ssp,
      back_matter: {
        ...backMatter,
        resources: [...resources, resource]
      }
    };

    await insert('system-security-plan', updatedSSP);
    onChange(resource.uuid);
    setTitle('');
    setHref('');
  };

  return (
    <IonList>
      <IonItem>
        <IonLabel position="stacked">Document Title</IonLabel>
        <IonInput
          value={title}
          onIonChange={e => setTitle(e.detail.value || '')}
          placeholder="Enter document title..."
          disabled={readonly}
        />
      </IonItem>

      <IonItem>
        <IonLabel position="stacked">Document URL</IonLabel>
        <IonInput
          value={href}
          onIonChange={e => setHref(e.detail.value || '')}
          placeholder="Enter document URL..."
          disabled={readonly}
        />
      </IonItem>

      <IonButton
        expand="block"
        onClick={handleAdd}
        disabled={!title || !href || readonly}
      >
        Add Document Reference
      </IonButton>

      {value && (
        <IonItem>
          <IonLabel>
            <IonText color="medium">Current Document Reference:</IonText>
            <p>{value}</p>
          </IonLabel>
        </IonItem>
      )}
    </IonList>
  );
};

export default IonDocumentResourceWidget;
