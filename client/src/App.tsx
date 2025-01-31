import {
  IonApp,
  IonRoute,
  IonRouterOutlet,
  IonSplitPane,
  setupIonicReact,
} from "@ionic/react";
import { IonReactHashRouter } from "@ionic/react-router";
import Menu from "./components/Menu";
import { ChatbotProvider } from "./context/ChatbotContext";
// import ChatbotWindow from "./components/chatbot/ChatbotWindow";
import Assess from "./pages/Assess";
import Authorize from "./pages/Authorize";
import Categorize from "./pages/Categorize";
import Implement from "./pages/Implement";
import ImportExport from "./pages/ImportExport";
import Documents from "./pages/Documents";
import Monitor from "./pages/Monitor";
import Prepare from "./pages/Prepare";
import Select from "./pages/Select";

/* Core CSS required for Ionic components to work properly */
import "@ionic/react/css/core.css";

/* Basic CSS for apps built with Ionic */
import "@ionic/react/css/normalize.css";
import "@ionic/react/css/structure.css";
import "@ionic/react/css/typography.css";

/* Optional CSS utils that can be commented out */
import "@ionic/react/css/display.css";
import "@ionic/react/css/flex-utils.css";
import "@ionic/react/css/float-elements.css";
import "@ionic/react/css/padding.css";
import "@ionic/react/css/text-alignment.css";
import "@ionic/react/css/text-transformation.css";

/**
 * Ionic Dark Mode
 * -----------------------------------------------------
 * For more info, please see:
 * https://ionicframework.com/docs/theming/dark-mode
 */

/* import '@ionic/react/css/palettes/dark.always.css'; */
/* import '@ionic/react/css/palettes/dark.class.css'; */
import "@ionic/react/css/palettes/dark.system.css";

/* Theme variables */
import Editor from "./pages/Editor";
import EditPage from "./pages/EditPage";
import Home from "./pages/Home";
import PrepareOrganization from "./pages/PrepareOrganization";
import PrepareSystem from "./pages/PrepareSystem";
import "./theme/variables.css";
import Assistance from "./pages/Assistance";
import Rmf from "./pages/Rmf";

setupIonicReact();

const App: React.FC = () => {
  return (
    <IonApp>
      {/* <ChatbotProvider> */}
        <IonReactHashRouter>
          <IonSplitPane when={false} contentId="main">
            <Menu />

            <IonRouterOutlet id="main">
              <IonRoute path="/" render={() => <Documents />} />
              {/* <IonRoute path="/editor" exact={true} render={() => <Editor />} />
              <IonRoute
                path="/edit/:type"
                exact={true}
                render={() => <EditPage />}
              /> */}
              {/* <IonRoute
                path="/prepare"
                exact={true}
                render={() => <Prepare />}
              />
              <IonRoute
                path="/rmf"
                exact={true}
                render={() => <Rmf />}
              />
              <IonRoute
                path="/organization-prep"
                exact={false}
                render={() => <PrepareOrganization />}
              />
              <IonRoute
                path="/system-prep"
                exact={false}
                render={() => <PrepareSystem />}
              />
              <IonRoute
                path="/select"
                exact={false}
                render={() => <Select />}
              />
              <IonRoute
                path="/categorize"
                exact={false}
                render={() => <Categorize />}
              />
              <IonRoute
                path="/implement"
                exact={false}
                render={() => <Implement />}
              />
              <IonRoute
                path="/assess"
                exact={false}
                render={() => <Assess />}
              />
              <IonRoute
                path="/authorize"
                exact={false}
                render={() => <Authorize />}
              />
              <IonRoute
                path="/assistance"
                exact={false}
                render={() => <Assistance />}
              />
              <IonRoute
                path="/monitor"
                exact={false}
                render={() => <Monitor />}
              /> */}
              {/* <IonRoute
                path="/documents"
                exact={false}
                render={() => <Documents />}
              />
              <IonRoute
                path="/import-export"
                exact={false}
                render={() => <ImportExport />}
              /> */}
            </IonRouterOutlet>
          </IonSplitPane>
        </IonReactHashRouter>
        {/* <ChatbotWindow /> */}
      {/* </ChatbotProvider> */}
    </IonApp>
  );
};

export default App;
