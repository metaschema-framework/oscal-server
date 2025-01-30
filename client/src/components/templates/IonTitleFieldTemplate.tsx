import React from 'react';
import { IonInput, IonTitle } from '@ionic/react';
import { TitleFieldProps } from '@rjsf/utils';
const IonTitleFieldTemplate: React.FC<TitleFieldProps> = ({
  id,
  required,
    title
}) => {

  return (
<IonTitle>{title}</IonTitle>
  );
};

export default IonTitleFieldTemplate;