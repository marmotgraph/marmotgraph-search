import {faBars} from '@fortawesome/free-solid-svg-icons/faBars';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {useLocation, useNavigate, useSearchParams} from 'react-router-dom';
import SignIn from '../../features/auth/SignIn';
import AuthEndpointAvailabilityBanner from '../../components/AuthEndpointAvailabilityBanner/AuthEndpointAvailabilityBanner';
import { reset } from '../../features/instance/instanceSlice';

import './Header.css';
import SearchBox from '../../features/search/SearchBox';

const Header = () => {

  const location = useLocation();
  const navigate = useNavigate();

  const dispatch = useDispatch();
  const configuration = useSelector(state => state.application.config);
  const theme = useSelector(state => state.application.theme);
  const [searchParams] = useSearchParams();
  const showSearchLink  = location.pathname.includes('/instances/');

  return (
    <div>
        <AuthEndpointAvailabilityBanner/>
    <nav className="navbar navbar-expand-lg navbar-light kgs-navbar">

      <div className="container-fluid">
        <a href={configuration.home} aria-label={configuration.name+` homepage`} className="logo nuxt-link-active navbar-brand">
          <img src={`/api/assets/img/${theme === 'dark'? configuration.logoDark : configuration.logo}`} alt={configuration.name} height="80" />
          <span> | Knowledge Graph Search</span>
        </a>
        <button className="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
          <FontAwesomeIcon icon={faBars} />
        </button>

        <div className="collapse navbar-collapse" id="navbarSupportedContent">
          <ul className="navbar-nav mr-auto">
            {showSearchLink && <SearchBox  callback={v=>{
              dispatch(reset());
              const newParams = new URLSearchParams(searchParams);
              newParams.set('q', v);
              navigate(`../search?${newParams.toString()}`);
            }} />}
            {configuration.navbarItems}
            <SignIn Tag="li" className="nav-item" />
          </ul>
        </div>
      </div>
    </nav>
    </div>
  );
};

export default Header;
